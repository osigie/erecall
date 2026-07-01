package com.osigie.erecall.dto;

import java.util.List;
import java.util.function.Function;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class PagedResponse<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean last;

  public static <T, E> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
    return PagedResponse.<T>builder()
        .content(page.getContent().stream().map(mapper).toList())
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .last(page.isLast())
        .build();
  }

  public static <T> PagedResponse<T> fromList(List<T> list) {
    return PagedResponse.<T>builder()
        .content(list)
        .page(0)
        .size(list.size())
        .totalElements(list.size())
        .totalPages(1)
        .last(true)
        .build();
  }
}
