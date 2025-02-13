package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.Feature;

import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

@Worker(goal = "Answer questions about time",
        background = """
                You are a professional chronologist and can answer questions about time.
                You can also perform various time-related tasks such as conversions and formatting.
                """)
public class Chronologist extends WorkerService {

    @Feature(name = "CurrentTime", description = "Return the current time in the format HH:mm:ss")
    public String currentTime() {
        LocalDateTime currentDate = LocalDateTime.now();
        return currentDate.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Feature(name = "CurrentTimeInLocation", description = "Return the current time for a location in the format HH:mm:ss")
    public String currentTime(String location) {
        addDateContext();
        addSystemMessage("Return the current time for a location in the format HH:mm:ss");
        addUserMessage("What is the current time in " + location + "?");
        Prompt prompt = new Prompt(getMessages());
        return callPromptForString(prompt);
    }

    @Feature(name = "CurrentDate", description = "Return the current date in the format yyyy-MM-dd")
    public String currentDate() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Feature(name = "CurrentDay", description = "Return the current day of the week")
    public String currentDay() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    @Feature(name = "CurrentMonth", description = "Return the name of the month")
    public String currentMonth() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    @Feature(name = "CurrentYear", description = "Return the current year")
    public Integer currentYear() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.getYear();
    }
}
