package org.cardanofoundation.job.util.report;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExportColumn {

  // export common
  private ColumnFieldEnum columnField;
  private ColumnTitleEnum columnTitle;

  // use for export excel
  private Alignment align;
  private int columnWidth;

  public enum Alignment {
    LEFT,
    RIGHT,
    CENTER
  }

  public ExportColumn(
      ColumnFieldEnum column, ColumnTitleEnum title, Alignment align, int columnWidth) {
    this.columnField = column;
    this.columnTitle = title;
    this.align = align;
    this.columnWidth = columnWidth;
  }

  public ExportColumn(ColumnFieldEnum column, ColumnTitleEnum title, Alignment align) {
    this.columnField = column;
    this.columnTitle = title;
    this.align = align;
  }

  public ExportColumn(ColumnFieldEnum column, ColumnTitleEnum title) {
    this.columnField = column;
    this.columnTitle = title;
    this.align = Alignment.LEFT;
  }
}
