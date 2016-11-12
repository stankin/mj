package ru.stankin.mj.http;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by nickl-mac on 09.11.15.
 */
public class LocalFilter implements Filter {
    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        //HttpServletRequest request = (HttpServletRequest) req;
        if (req.getServerName().equals("localhost"))
            //((HttpServletResponse) resp).sendError(403, "No access");
            chain.doFilter(req, resp);
        else
            ((HttpServletResponse) resp).sendRedirect("http://inteh-info.ru/studentam/porjadok-informirovanija-o-rezultatakh-attestatsijj/");
    }

    public void init(FilterConfig config) throws ServletException {

    }

}
