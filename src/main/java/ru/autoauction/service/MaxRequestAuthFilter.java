package ru.autoauction.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class MaxRequestAuthFilter extends OncePerRequestFilter {
  public static final String HEADER="X-Max-Init-Data";
  private final MaxAuthService auth;
  public MaxRequestAuthFilter(MaxAuthService auth){this.auth=auth;}
  @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().startsWith("/api/");}
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
    HttpSession session=request.getSession(false);
    if(session==null||!(session.getAttribute(CurrentUser.SESSION_KEY) instanceof Long)){
      String initData=request.getHeader(HEADER);
      if(initData!=null&&!initData.isBlank()){
        try{var user=auth.authenticate(initData);request.getSession(true).setAttribute(CurrentUser.SESSION_KEY,user.id);}catch(RuntimeException ignored){}
      }
    }
    chain.doFilter(request,response);
  }
}
