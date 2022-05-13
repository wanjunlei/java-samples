package com.openfunction.java;

import com.openfunction.functions.HttpFunction;
import com.openfunction.functions.HttpRequest;
import com.openfunction.functions.HttpResponse;

public class HelloWorld implements HttpFunction {

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {

        response.getWriter().write("hello world");
    }
}

