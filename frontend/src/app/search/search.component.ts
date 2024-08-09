import {Component} from "@angular/core";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {FormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatIconModule} from "@angular/material/icon";
import {MatDividerModule} from "@angular/material/divider";
import {SearchService} from "./search.service";
import {Response} from "./response";
import {NgForOf} from "@angular/common";
import {MatExpansionPanelTitle} from "@angular/material/expansion";
import {MatCard, MatCardContent} from "@angular/material/card";
import {
      MatCell,
      MatCellDef,
      MatColumnDef,
      MatHeaderCell,
      MatHeaderCellDef,
      MatHeaderRow,
      MatHeaderRowDef,
      MatRow,
      MatRowDef,
      MatTable
} from "@angular/material/table";
import {TooltipComponent} from "@angular/material/tooltip";

@Component({
      selector: 'app-search',
      standalone: true,
      imports: [MatFormFieldModule, MatInputModule, FormsModule, MatButtonModule, MatIconModule, MatDividerModule, NgForOf, MatExpansionPanelTitle, MatCard, MatCardContent, MatTable, MatColumnDef, MatHeaderCell, MatCell, MatRow, MatHeaderRow, MatCellDef, MatHeaderCellDef, MatHeaderRowDef, MatRowDef, TooltipComponent],
      providers: [SearchService],
      templateUrl: './search.component.html',
      styleUrl: './search.component.css'
})
export class SearchComponent {
      data: Response[] = [];
      value = '';
      displayedColumns: string[] = ['Search-Accuracy', 'Response'];
      constructor(private searchService: SearchService) {
      }

      search(searchString: string) {

            this.searchService.search(searchString)
                  .subscribe(res => this.data= res);
      }


}
