package com.sidutti.charlie.model;

import java.util.List;


public record Root(List<Feature> features,
                   List<RootRow> rows,
                   int num_rows_total,
                   int num_rows_per_page,
                   boolean partial) {

    public record Feature(int feature_idx,
                          String name,
                          Type type) {

    }

    public record RootRow(int row_idx,
                          Row row,
                          List<Object> truncated_cells) {

    }

    public record Row(String instruction,
                      String output,
                      String input) {

    }

    public record Type(String dtype,
                       String _type) {

    }
}




