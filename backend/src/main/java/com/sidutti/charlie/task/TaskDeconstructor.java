package com.sidutti.charlie.task;

import com.sidutti.charlie.service.NlpService;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class TaskDeconstructor {

    private final NlpService nlpService;

    public TaskDeconstructor(NlpService nlpService) {
        this.nlpService = nlpService;
    }

    public List<Task> deconstruct(List<Task> tasks) {
        List<Task> subtasks = tasks.stream()
                .map(Task::getDescription)
                .flatMap(s -> nlpService.getSubtasks(s).stream())
                .map(Task::new)
                .toList();

        List<Task> result;
        if (subtasks.isEmpty()) {
            result = tasks;
        } else if (subtasks.size() == 1) {
            result = subtasks;
        } else {
            result = subtasks.subList(1, subtasks.size());
        }

        return result;
    }
}
