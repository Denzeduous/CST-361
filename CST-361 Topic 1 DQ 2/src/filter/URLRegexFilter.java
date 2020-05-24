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
 * Servlet Filter implementation class URLRegexFilter
 */
@WebFilter("/URLRegexFilter")
public class URLRegexFilter implements Filter {

    /**
     * Default constructor. 
     */
    public URLRegexFilter() {}

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
        
        // This was made by me using the URL specification at: https://en.wikipedia.org/wiki/URL#Syntax,
        // the fragment identifier examples at: https://en.wikipedia.org/wiki/Fragment_identifier#Examples,
        // the IPv6 specifications at: https://en.wikipedia.org/wiki/IPv6,
        // and this IPv6 image from Oracle's documentation at: https://docs.oracle.com/cd/E23823_01/html/816-4554/figures/basic-IPv6-address.png
        Pattern p = Pattern.compile(
            "([\\w\\+\\.\\-]+:)?" + // Optional scheme.
                                    // Made optional only since modern browsers assume
                                    //`https://` when left out
            
            // Authority
            "(\\/\\/([\\s\\S]:[\\s\\S]?@)?)?" + // Optional authority
            
            // Path (can either be basic, IPv4, or IPv6 surrounded in square brackets)
            "((\\w+(\\.\\w+)+)|" + // Basic path (ex: google.com)
                "([0-9]{1,3}(\\.[0-9]{1,3}){3})|" + // IPv4 address using dot-decimal notation
            
                    // IPv6 (surrounded in square brackets)
                    //
                    // Known issue: it can't correctly check the amount. It will definitely stop them from being enormous, but
                    // due to the nature of `::`, it's impossible to make regex check how many iterations there were to make sure
                    // it stays under the limit. It'll generally be close, however.
                    //
                    // There's also no way to make sure it's under the limit, so it's very possible to go under it.
                    "(\\[((([a-fA-F0-9]{1,4}::?){1,7}([a-fA-F0-9]{1,4}))|" + // They can either be single-colon `:` or double `::` when
                                                                             // excluding IDs that are `0`
                        "((::[a-fA-F0-9]{1,4})(::?[a-fA-F0-9]{1,4}){0,6}))" + // If the address starts with `::`
                    "\\]))" + // Closing the IPv6 check
                   
            "(:[0-9]{1,5})?" + // Optional port
            "(\\/+\\S*)*" + // Optional path
            "((\\?)(\\w+\\=\\w*)(&\\w+\\=\\w*)*)?" + // Optional query. Does not allow `;` as the semicolon was made
                                                     // obsolete by RFC 2854
            "(#" + // Optional fragment component
                "((\\S+)|" + // HTML5-compatible identifier
                    "(:~:text=[\\S]+)?))?" // From the ScrollToTextFragment proposal. Supported in Chrome versions 80 and above
        );
        
        Matcher m = p.matcher(input);
        
        if (m.matches()) {
            
            // This was gotten from https://stackoverflow.com/a/24486152
            response.resetBuffer();
            response.getOutputStream().write("<p>The input text was a valid URL.</p>".getBytes());
            
            return;
        }

        // pass the request along the filter chain
        chain.doFilter(request, response);
    }
}
