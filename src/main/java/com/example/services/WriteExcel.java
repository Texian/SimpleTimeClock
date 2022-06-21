package com.example.services;

import com.example.entities.TimeEntry;
import jxl.CellView;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.UnderlineStyle;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
public class WriteExcel{
    private WritableCellFormat timesBoldUnderline;
    private WritableCellFormat times;

    public java.io.InputStream exportExcel(List<TimeEntry> list) {
        try {
            java.io.InputStream is = write(list);
            return is;
        } catch(WriteException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.io.InputStream write(List<TimeEntry> list) throws WriteException, IOException {
        java.io.OutputStream os = new java.io.ByteArrayOutputStream();
        WorkbookSettings ws = new WorkbookSettings();
        ws.setLocale(new Locale("en", "EN"));

        WritableWorkbook workbook = Workbook.createWorkbook(os, ws);
        workbook.createSheet("Report", 0);
        WritableSheet excelSheet = workbook.getSheet(0);
        createLabel(excelSheet);
        int size = createContent(excelSheet, list);

        workbook.write();
        workbook.close();

        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        stream = (java.io.ByteArrayOutputStream) os;
        byte[] bytes = stream.toByteArray();
        java.io.InputStream is = new java.io.ByteArrayInputStream(bytes);

        return is;
    }

    private void createLabel(WritableSheet sheet) throws WriteException {
        // create format for header cells
        WritableFont times10pt = new WritableFont(WritableFont.TIMES, 10);
        times = new WritableCellFormat(times10pt);
        times.setWrap(true);

        WritableFont times10ptBoldUnderline = new WritableFont(WritableFont.TIMES, 10, WritableFont.BOLD, false,
                UnderlineStyle.SINGLE);
        timesBoldUnderline = new WritableCellFormat(times10ptBoldUnderline);
        timesBoldUnderline.setWrap(true);

        CellView cv = new CellView();
        cv.setFormat(times);
        cv.setFormat(timesBoldUnderline);
        cv.setAutosize(true);

        addCaption(sheet, 0, 0, "Writer");
        addCaption(sheet, 1, 0, "Date");
        addCaption(sheet, 2, 0, "Guide");
        addCaption(sheet, 3, 0, "Description");
        addCaption(sheet, 4, 0, "Status");
    }

    private int createContent(WritableSheet sheet, List<TimeEntry> list) throws WriteException {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            TimeEntry te = list.get(i);

            String name = te.getName();
            String date = te.getDate();
            String guide = te.getHours();
            String description = te.getDescription();
            String status = te.getStatus();

            addLabel(sheet, 0, i+2, name);
            addLabel(sheet, 1, i+2, date);
            addLabel(sheet, 2, i+2, guide);
            addLabel(sheet, 3, i+2, description);
            addLabel(sheet, 4, i+2, status);
            i++;
        }
        return size;
    }

    private void addCaption(WritableSheet sheet, int column, int row, String s) throws WriteException {
        Label label;
        label = new Label(column, row, s, timesBoldUnderline);
        int cc = countString(s);
        sheet.setColumnView(column, cc);
        sheet.addCell(label);
    }

    private void addNumber(WritableSheet sheet, int column, int row, Integer i) throws WriteException {
        Number number;
        number = new Number(column, row, i, times);
        sheet.addCell(number);
    }

    private void addLabel(WritableSheet sheet, int column, int row, String s) throws WriteException {
        Label label;
        label = new Label(column, row, s, times);
        int cc = countString(s);
        if (cc > 200) {
            sheet.setColumnView(column, 150);
        } else {
            sheet.setColumnView(column, cc+6);
        }
        sheet.addCell(label);
    }

    private int countString(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                count++;
            }
        }
        return count;
    }
}
