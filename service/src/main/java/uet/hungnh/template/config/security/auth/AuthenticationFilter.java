package uet.hungnh.template.config.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;
import uet.hungnh.template.config.security.model.TokenResponse;
import uet.hungnh.template.controller.APIController;

import javax.security.sasl.AuthenticationException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class AuthenticationFilter extends GenericFilterBean {

    private final static Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private AuthenticationManager authenticationManager;

    public AuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = asHttp(request);
        HttpServletResponse httpResponse = asHttp(response);

        Optional<String> username = Optional.ofNullable(httpRequest.getHeader("X-Auth-Username"));
        Optional<String> password = Optional.ofNullable(httpRequest.getHeader("X-Auth-Password"));
        Optional<String> token = Optional.ofNullable(httpRequest.getHeader("X-Auth-Token"));

        String resourcePath = (new UrlPathHelper()).getPathWithinApplication(httpRequest);

        try {
            if (postToAuthenticate(httpRequest, resourcePath)) {
                logger.debug("Trying to authenticate user {} by username/password method ", username);
                processUsernamePasswordAuthentication(httpRequest, httpResponse, username, password);
                return;
            }

            if (token.isPresent()) {
                logger.debug("Trying to authenticate user by Token {} ", token);
                processTokenAuthentication(token);
            }

            logger.debug("AuthenticationFilter is passing request down the filter chain");
            chain.doFilter(request, response);
        }
        catch (InternalAuthenticationServiceException e) {
            SecurityContextHolder.clearContext();
            logger.error("Internal authentication service exception", e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
        catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private void processTokenAuthentication(Optional<String> token) {
        Authentication authenticationResult = tryToAuthenticateWithToken(token);
        SecurityContextHolder.getContext().setAuthentication(authenticationResult);
    }

    private Authentication tryToAuthenticateWithToken(Optional<String> token) {
        PreAuthenticatedAuthenticationToken authenticationRequest = new PreAuthenticatedAuthenticationToken(token, null);
        return tryToAuthenticate(authenticationRequest);
    }

    private void processUsernamePasswordAuthentication(HttpServletRequest request,
                                                       HttpServletResponse response,
                                                       Optional<String> username,
                                                       Optional<String> password) throws IOException {
        if (!username.isPresent() || !password.isPresent()) {
            throw new InternalAuthenticationServiceException("Unable to authenticate without username or password!");
        }

        Authentication authenticationResult = tryToAuthenticateWithUsernameAndPassword(username, password);
        SecurityContextHolder.getContext().setAuthentication(authenticationResult);

        response.setStatus(HttpServletResponse.SC_OK);

        TokenResponse tokenResponse = new TokenResponse(authenticationResult.getDetails().toString());
        String tokenResponseJson = (new ObjectMapper().writeValueAsString(tokenResponse));
        response.addHeader("Content-Type", "application/json");
        response.getWriter().print(tokenResponseJson);
    }

    private Authentication tryToAuthenticateWithUsernameAndPassword(Optional<String> username, Optional<String> password) {
        UsernamePasswordAuthenticationToken authenticationRequest = new UsernamePasswordAuthenticationToken(username, password);
        return tryToAuthenticate(authenticationRequest);
    }

    private Authentication tryToAuthenticate(Authentication authenticationRequest) {
        Authentication authenticationResult = authenticationManager.authenticate(authenticationRequest);
        if (authenticationResult == null || !authenticationResult.isAuthenticated()) {
            throw new InternalAuthenticationServiceException("Unable to authenticate user for provided credentials!");
        }
        logger.debug("User is successfully authenticated!");
        return authenticationResult;
    }

    private boolean postToAuthenticate(HttpServletRequest httpRequest, String resourcePath) {
        return (
                "POST".equals(httpRequest.getMethod())
                && APIController.AUTHENTICATION_ENDPOINT.equals(resourcePath)
        );
    }

    private HttpServletResponse asHttp(ServletResponse response) {
        return (HttpServletResponse) response;
    }

    private HttpServletRequest asHttp(ServletRequest request) {
        return (HttpServletRequest) request;
    }


}
