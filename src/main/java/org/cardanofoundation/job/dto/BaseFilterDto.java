package org.cardanofoundation.job.dto;

import java.io.Serializable;
import java.util.List;

import lombok.*;

import org.springframework.data.domain.Page;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseFilterDto<T> implements Serializable {

  private List<T> data;
  private long totalItems;
  private int totalPages;
  private int currentPage;

  public BaseFilterDto(Page<T> page) {
    this.data = page.getContent();
    this.totalItems = page.getTotalElements();
    this.totalPages = page.getTotalPages();
    this.currentPage = page.getNumber();
  }

  public <S> BaseFilterDto(Page<S> page, List<T> data) {
    this.data = data;
    this.totalItems = page.getTotalElements();
    this.totalPages = page.getTotalPages();
    this.currentPage = page.getNumber();
  }

  public <S> BaseFilterDto(List<T> data, long totalItems) {
    this.data = data;
    this.totalItems = totalItems;
  }
}
