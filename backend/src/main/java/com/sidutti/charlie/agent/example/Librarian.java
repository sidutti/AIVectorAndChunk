package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;

@Worker(goal = "Answer questions about books and authors",
        background = "You are a helpful librarian and can answer a variety of questions about books and authors")
public class Librarian extends WorkerService {
}
