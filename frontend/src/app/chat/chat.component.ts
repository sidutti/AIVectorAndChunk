import {Component} from '@angular/core';
import {FormsModule} from "@angular/forms";
import {MatButton} from "@angular/material/button";
import {MatCard, MatCardContent} from "@angular/material/card";
import {MatFormField, MatLabel, MatSuffix} from "@angular/material/form-field";
import {MatInput} from "@angular/material/input";
import {ChatService} from "./chat.service";
import {NgIf} from "@angular/common";

@Component({
      selector: 'app-chat',
      standalone: true,
      imports: [
            FormsModule,
            MatButton,
            MatCard,
            MatCardContent,
            MatFormField,
            MatInput,
            MatLabel,
            MatSuffix,
            NgIf
      ],
      providers: [ChatService],
      templateUrl: './chat.component.html',
      styleUrl: './chat.component.css'
})
export class ChatComponent {
      value = '';
      data: string = '';

      constructor(private chatService: ChatService) {
      }

      search(searchString: string) {
            this.chatService.search(searchString)
                  .subscribe(res => this.data = res);
      }
}
