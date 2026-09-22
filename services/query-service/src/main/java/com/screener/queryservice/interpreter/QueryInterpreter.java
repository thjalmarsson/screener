package com.screener.queryservice.interpreter;

import com.screener.queryservice.client.StockScreenRequest;

public interface QueryInterpreter {

    QueryInterpretation interpret(String query);
}
