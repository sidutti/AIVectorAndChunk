import {Component} from '@angular/core';
import {SearchComponent} from "./search/search.component";


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [SearchComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {


}





