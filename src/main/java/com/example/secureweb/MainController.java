package com.example.secureweb;

import com.example.entities.TimeEntry;
import com.example.services.DynamoDBService;
import com.example.services.SendMessages;
import com.example.services.WriteExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

// Class handles http requests to pull datasets from the time clock table
@Controller
public class MainController{

    @Autowired
    DynamoDBService dbService;

    @Autowired
    SendMessages sendMsg;

    @Autowired
    WriteExcel excel;

    @GetMapping("/")
    public String root() {
        return"index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/add")
    public String designer() {
        return "add";
    }

    @GetMapping("/items")
    public String items() {
        return "items";
    }

    // Adding new items to the database
    @RequestMapping(value="/add", method= RequestMethod.POST)
    @ResponseBody
    String addItems(HttpServletRequest req, HttpServletResponse res) {
        // Get the user
        String name = getLoggedInUser();
        String guide = req.getParameter("guide");
        String description = req.getParameter("description");
        String status = req.getParameter("status");

        // Create objects, pass to injectNewSubmission()
        TimeEntry myTime = new TimeEntry();
        myTime.setName(name);
        myTime.setGuide(guide);
        myTime.setDescription(description);
        myTime.setStatus(status);

        dbService.setItem(myTime);
        return "Time logged";
    }

    // Builds an email report from the database
    @RequestMapping(value="/report", method= RequestMethod.POST)
    @ResponseBody
    String getReport(HttpServletRequest req, HttpServletResponse res) {

        String email = req.getParameter("email");
        List<TimeEntry> entries = dbService.getListItems();
        java.io.InputStream is = excel.exportExcel(entries);

        try {
            sendMsg.sendReport(is, email);
        } catch (IOException e) {
            e.getStackTrace();
        }
        return "Report sent";
    }

    // Archive a sheet
    @RequestMapping(value="/archive", method= RequestMethod.POST)
    @ResponseBody
    String archiveTime(HttpServletRequest req, HttpServletResponse res) {
        String id = req.getParameter("id");
        dbService.archiveTime(id);
        return id;
    }

    // Get sheet
    @RequestMapping(value="/retrieve", method= RequestMethod.POST)
    @ResponseBody
    String retrieveTime(HttpServletRequest req, HttpServletResponse res) {
        String type = req.getParameter("type");
        String xml = "";

        if (type.compareToIgnoreCase("archive")==0) {
            xml = dbService.getClosedItems();
        } else {
            xml = dbService.getOpenItems();
        }
        return xml;
    }

    // Modify sheet
    @RequestMapping(value="/modify", method= RequestMethod.POST)
    @ResponseBody
    String modifyTime(HttpServletRequest req, HttpServletResponse res) {
        String id = req.getParameter("id");
        return dbService.getItem(id);
    }

    private String getLoggedInUser() {
        org.springframework.security.core.userdetails.User user2 = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user2.getUsername();
    }
}
