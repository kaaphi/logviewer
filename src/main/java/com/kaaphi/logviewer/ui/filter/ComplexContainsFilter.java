package com.kaaphi.logviewer.ui.filter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.kaaphi.logviewer.LogFile.Filter;

/**
 * This is a filter whose filter string is a set of simple contains filter
 * strings separated by AND, OR, or NOT operators in infix notation. Parentheses for
 * grouping operations are allowed. Operators, parentheses, and backslashes can
 * be escaped with a backslash. The filter is case-insensitive.
 * 
 * Example: bob&(apple|orange)
 * 
 * Will filter to all lines that have "bob" and also have either "apple" or
 * "orange" in them.
 */
public class ComplexContainsFilter implements Filter {
	private Predicate<String> predicate;
	
	private static enum Paren {
		OPEN,
		CLOSE
	}
	
	private static enum Operator {
		AND,
		OR,
		NOT(1,1),
		;
		private final int numArgs;
		private final int precedence;
		private Operator(int precedence, int numArgs) {
			this.precedence = precedence;
			this.numArgs = numArgs;
		}
		private Operator() {
			this(0, 2);
		}
		public int getNumArgs() { return numArgs; }
		public boolean isLessThanOrEqual(Operator o2) {
			return precedence <= o2.precedence;
		}
	}
	
	public ComplexContainsFilter(String filterString) {
		this.predicate = parse(
				cheapLexer(filterString).iterator(), 
				ComplexContainsFilter::contains, 
				Predicate::or, 
				Predicate::and,
				Predicate::negate);
	}
		
	@Override
	public boolean filter(String line) {
		return predicate.test(line.toLowerCase());
	}	
	
	/**
	 * This parses an iterator of tokens (lexed using the
	 * {@link #cheapLexer(String)} below) into some output. It processes the
	 * infix notation into postfix notation using the Shunting-yard algorithm.
	 * The postfix notation is evaluated in real time.
	 * 
	 * The function arguments are used to make this algorithm more easily
	 * testable since the actual usage is to produce composed Predicates which
	 * aren't easily examined for correctness.
	 * 
	 * @param tokens
	 * @param mapper
	 * @param or
	 * @param and
	 * @return
	 */
	private static <T> T parse(Iterator<Object> tokens, Function<String, T> mapper, BiFunction<T, T, T> or, BiFunction<T, T, T> and, Function<T, T> not) {
		Deque<Object> parseStack = new LinkedList<>();
		Deque<T> outputStack = new LinkedList<>();
		while(tokens.hasNext()) {
			Object token = tokens.next();
			if(token instanceof String) {
				outputStack.push(mapper.apply((String)token));
			}
			else if(token instanceof Operator) {
				while(!parseStack.isEmpty() 
						&& parseStack.peek() instanceof Operator 
						&& ((Operator)token).isLessThanOrEqual((Operator)parseStack.peek())
						) {
					evaluateParseStack(parseStack, outputStack, or, and, not);
				}
				parseStack.push(token);
			}
			else if(token instanceof Paren) {
				switch((Paren)token) {
				case OPEN:
					parseStack.push(token);
					break;
				case CLOSE:
					while(!parseStack.isEmpty() && parseStack.peek() != Paren.OPEN) {
						evaluateParseStack(parseStack, outputStack, or, and, not);
					}
					if(parseStack.isEmpty()) {
						throw new IllegalArgumentException("Mismatched parens!");
					} else {
						parseStack.pop();
					}
					break;
				default: throw new IllegalArgumentException(token.toString());
				}
			}
		}
		
		while(!parseStack.isEmpty()) {
			if(parseStack.peek() instanceof Operator) {
				evaluateParseStack(parseStack, outputStack, or, and, not);
			} else {
				throw new IllegalArgumentException("Mismatched parens!");
			}
		}
		
		if(outputStack.size() != 1) {
			throw new IllegalArgumentException(String.format("Output stack too large: %s", outputStack));
		}
		
		return outputStack.pop();
	}
	
	private static <T> void evaluateParseStack(Deque<Object> parseStack, Deque<T> outputStack, BiFunction<T, T, T> or, BiFunction<T, T, T> and, Function<T, T> not) {
		Operator op = (Operator)parseStack.pop();
		List<T> args = new ArrayList<T>(op.getNumArgs());
		for(int i = 0; i < op.getNumArgs(); i++) {
			args.add(outputStack.pop());
		}
		switch(op) {
		case OR: 
			outputStack.push(or.apply(args.get(0), args.get(1)));
			break;
		case AND: 
			outputStack.push(and.apply(args.get(0), args.get(1)));
			break;
		case NOT:
			outputStack.push(not.apply(args.get(0)));
			break;
		default: throw new IllegalArgumentException(op.toString());
		}
	}
	
	/*
	 * This is a cheap handrolled lexer that lexes the input string into
	 * contains strings, operators, and parens. Will handle backslash-escaped
	 * operators and parens.
	 */
	private static Stream<Object> cheapLexer(String str) {
		Stream.Builder<Object> builder = Stream.builder();
		char[] c = str.toCharArray();
		int max = str.length();
		StringBuilder sb = null;
		for(int i = 0; i < max; i++) {
			switch(c[i]) {
			case '(':
				processNonString(builder, sb, Paren.OPEN);
				sb = null;
				break;

			case ')':
				processNonString(builder, sb,  Paren.CLOSE);
				sb = null;
				break;
				
			case '|':
				processNonString(builder, sb, Operator.OR);
				sb = null;
				break;

			case '&':
				processNonString(builder, sb, Operator.AND);
				sb = null;
				break;
				
			case '!':
				processNonString(builder, sb, Operator.NOT);
				sb = null;
				break;
			
			case '\\':
				i++;
			default:
				if(sb == null) sb = new StringBuilder();
				sb.append(c[i]);
			}
		}
		if(sb != null) {
			builder.add(sb.toString().toLowerCase());
		}
		
		return builder.build();
	}
	
	private static void processNonString(Stream.Builder<Object> builder, StringBuilder sb, Object token) {
		if(sb != null) {
			builder.add(sb.toString().toLowerCase());
		}
		builder.add(token);
	}
	
	
	public static Predicate<String> contains(String str) {
		return line -> line.contains(str);
	}
	
	public static void main(String[] args) {
		//String query = "(efg|xyz|123)&abcd";
		String query = "cat&!gorge";
		System.out.println(query);
		System.out.println(parse(
				cheapLexer(query).iterator(), 
				Function.identity(), 
				(a,b) -> String.format("or(%s,%s)", a,b), 
				(a,b) -> String.format("and(%s,%s)", a,b),
				(a) -> String.format("not(%s)", a)
				)
		);
	}
}
