package com.sidutti.charlie.agent.example;

import com.sidutti.charlie.agent.Worker;
import com.sidutti.charlie.agent.WorkerService;

import com.sidutti.charlie.agent.example.model.MathRequest;
import com.sidutti.charlie.tool.Feature;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Worker(goal = "Answer mathematical questions and solve problems")
@RestController
@RequestMapping("/api/agents/mathematician")
public class Mathematician extends WorkerService {

    @Feature(name = "Adder", description = "Add a list of numbers together")
    @PostMapping("/add")
    public double add(@RequestBody MathRequest request) {
        double sum = 0;
        for (double num : request.numbers()) {
            sum += num;
        }
        return sum;
    }

    @Feature(name = "Subtractor", description = "Subtract a list of numbers")
    public double subtract(MathRequest request) {
        if (request.numbers().isEmpty()) return 0;
        double result = request.numbers().getFirst();
        for (int i = 1; i < request.numbers().size(); i++) {
            result -= request.numbers().get(i);
        }
        return result;
    }

    @Feature(name = "Multiplier", description = "Multiply a list of numbers together")
    public double multiply(MathRequest request) {
        if (request.numbers().isEmpty()) return 0;
        double result = 1;
        for (double num : request.numbers()) {
            result *= num;
        }
        return result;
    }

    @Feature(name = "Divider", description = "Divide a list of numbers")
    public double divide(MathRequest request) {
        if (request.numbers().isEmpty()) return Double.NaN;
        double result = request.numbers().getFirst();
        for (int i = 1; i < request.numbers().size(); i++) {
            if (request.numbers().get(i) == 0) {
                return Double.NaN; // Return NaN if division by zero is attempted
            }
            result /= request.numbers().get(i);
        }
        return result;
    }

    @Feature(name = "Square", description = "Square each of the list of numbers")
    public List<Double> square(MathRequest request) {
        return request.numbers().stream()
                .map(num -> num * num)
                .collect(Collectors.toList());
    }

    @Feature(name = "SquareRoot", description = "Calculate the square root of each number")
    public List<Double> squareRoot(MathRequest request) {
        return request.numbers().stream()
                .map(Math::sqrt)
                .collect(Collectors.toList());
    }
}
