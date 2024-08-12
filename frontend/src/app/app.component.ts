import {Component} from '@angular/core';
import {SearchComponent} from "./search/search.component";
import {TabsComponent} from "./tabs/tabs.component";
import {ChatComponent} from "./chat/chat.component";


@Component({
      selector: 'app-root',
      standalone: true,
      imports: [SearchComponent, ChatComponent,TabsComponent],
      templateUrl: './app.component.html',
      styleUrl: './app.component.css'
})
export class AppComponent {


}





