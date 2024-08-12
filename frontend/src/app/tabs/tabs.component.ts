import { Component } from '@angular/core';
import {MatTab, MatTabGroup} from "@angular/material/tabs";
import {SearchComponent} from "../search/search.component";
import {ChatComponent} from "../chat/chat.component";

@Component({
  selector: 'app-tabs',
  standalone: true,
      imports: [
            MatTabGroup,
            MatTab,
            SearchComponent,
            ChatComponent
      ],
  templateUrl: './tabs.component.html',
  styleUrl: './tabs.component.css'
})
export class TabsComponent {

}
