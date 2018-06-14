# spring_cloud_actuator_improve
add request and response body in trace endpoint

The code is very ugly.

I have tried several ways to write this code, but only this ungly one succeed.

1. Read request body manually before invoking super.doFilterInternal.
code:       String jsonBody =  org.apache.commons.io.IOUtils.toString(request.getInputStream());
            super.doFilterInternal(request, response, filterChain);
      
result: After reading request body manually, the request body will be marked "read", and later functions (super.doFilterInternal) won't get
        request body, so spring will give me exception "missing request body".
      

2. Invoke filterChain.doFilter and super.doFilterInternal.
Code:       filterChain.doFilter(requestWrapper, responseWrapper);
            request.setAttribute(REQUEST_BODY, getRequestBody(requestWrapper));
            request.setAttribute(RESPONSE_BODY, getResponseBody(responseWrapper));
            request.setAttribute(STATUS_CODE, responseWrapper.getStatusCode());
            super.doFilterInternal(request, response, filterChain);

Result: Invoke filterChain.doFilter will clear request body, so the second "filterChain.doFilter" which is invoke inside super.doFilterInternal,
will throw "missing request body" exception. Again, request body can only read once.

3. Current solution, a very ugly one. 
   I do not invoke super.doFilterInternal, just copy code from super.doFilterInternal into doFilterInternal.
   However, I need to add request/response body into repository, which is private variable in WebRequestTraceFilter.
   So, I create MyInMemoryTraceRepository.java, which is a copy of InMemoryTraceRepository.java.
   I inject MyInMemoryTraceRepository into RequestTraceFilter, and add request/response body into this repository.
   By doing this, I work around the private repository in WebRequestTraceFilter.

If I can find a way to read the request body, but not clear it, and later functions can still get request body, then I will use solution 1.
Solution 2 is incorrect, should not invoke doFilter twice, otherwise the request will be handled twice, just like we get two HTTP request.

Also note that, I use GZIPInputStream to unzip the response body. Because response body is zipped by Hujiang framework.
