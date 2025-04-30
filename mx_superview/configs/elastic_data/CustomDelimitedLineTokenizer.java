package com.finsurge.tmr_portal.mx_superview.configs.elastic_data;

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

import java.util.ArrayList;
import java.util.List;

public class CustomDelimitedLineTokenizer extends DelimitedLineTokenizer {

    @Override
    protected List<String> doTokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();

        for (char c : line.toCharArray()) {

            if (c == '~') {
                tokens.add(currentToken.toString());
                currentToken.setLength(0);
            } else if (c == '\'' || c == '"') {
                currentToken.append(c);
            } else {
                currentToken.append(c);
            }
        }
        tokens.add(currentToken.toString());
        return tokens;
    }

}