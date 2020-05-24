package filter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

/**
 * Servlet Filter implementation class NumberRegexFilter
 */
@WebFilter("/NumberRegexFilter")
public class NumberRegexFilter implements Filter {

    /**
     * Default constructor. 
     */
    public NumberRegexFilter() {}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {}
	
	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String input = request.getParameter("input");
		
		if (input == null || input.equals("")) {
			
			// This was gotten from https://stackoverflow.com/a/24486152
			response.resetBuffer();
			response.getOutputStream().write("<p>The input text was null or empty.</p>".getBytes());
			
			return;
		}
		
		Pattern p = Pattern.compile("-?[0-9]*(\\.[0-9]+)?");
		
		Matcher m = p.matcher(input);
		
		if (m.matches()) {
			
			// This was gotten from https://stackoverflow.com/a/24486152
			response.resetBuffer();
			response.getOutputStream().write("<p>The input text was a valid number.</p>".getBytes());
			
			return;
		}

		// pass the request along the filter chain
		chain.doFilter(request, response);
	}
}
