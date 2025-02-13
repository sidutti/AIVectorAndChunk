package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;
import com.sidutti.charlie.tool.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Worker(goal = "Provide useful answers about the host computer including cpus, files and directories")

public class ComputerAssistant extends WorkerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputerAssistant.class);
    @Feature(name = "DirectoryReader", description = "Given a directory, list all the files in the directory.")
    public List<String> readDirectory(String path) {
        File f = new File(path);
        List<String> out = new ArrayList<>();
        if (f.exists()) {
            String[] list = f.list();
            if (list != null) {
                out = Arrays.asList(list);
            }
        }
        return out;
    }

    @Feature(name = "CPU Analyzer", description = "Describe the number of cpus")
    public int cpuInfo() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Feature(name = "RAM memory analyzer", description = "Describe the total RAM")
    public String ramMemory() {
        String out = "Unable to get RAM info";
        try {
            String command = "grep MemTotal /proc/meminfo";
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = reader.readLine(); // Read only the first line
            if (line != null) {
                String[] parts = line.split("\\s+");
                long totalMemoryKb = Long.parseLong(parts[1]);
                out = String.format("Total RAM: %d GB", totalMemoryKb / 1024 / 1024);
            }
            reader.close();

        } catch (IOException e) {
            LOGGER.error("Error getting RAM memory", e);
        }
        return out;
    }

    @Feature(name = "JVM Memory Analyzer", description = "Describe the memory available to the JVM")
    public Map<String, Long> memoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
                "total", runtime.totalMemory(),
                "free", runtime.freeMemory(),
                "used", runtime.totalMemory() - runtime.freeMemory(),
                "max", runtime.maxMemory()
        );
    }
}
