import { Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { PlateformeService } from 'src/app/services/platefrome.service';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-plateforme',
  templateUrl: './plateforme.component.html',
  styleUrls: ['./plateforme.component.scss']
})
export class PlateformeComponent {
  type = 'git'; // This will make "GitHub" the default visible option

  plateformes: any[] = [];
  selectedPlateforme: any = null; 
  isModalOpen: boolean = false; 

  constructor(private plateformeService: PlateformeService) {}

  ngOnInit(): void {
    this.fetchPlateformes();
  }

  fetchPlateformes(): void {
    this.plateformeService.getAllPlateformes().subscribe(
      (data) => {
        this.plateformes = data.results; // Refreshing the list of plateformes
      },
      (error) => {
        console.error('Error fetching plateformes', error);
      }
    );
  }
  getPlateformeTypeLabel(type: string): string {
    if (type === 'http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#classroom') {
      return 'Classroom';
    } else if (type === 'http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#git') {
      return 'Github';
    } else if (type === 'http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#teams') {
      return 'Teams';
    } else if (type === 'http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#moodle') {
      return 'Moodle';
    }
    return 'Unknown Type'; // Default if no match
  }
  
  addPlateforme(form: NgForm) {
    const newPlateforme = form.value;
    this.plateformeService.addPlateforme(newPlateforme).subscribe(
      (response) => {
        console.log('Plateforme added successfully', response);
        form.reset(); // Reset the form after submission
        this.fetchPlateformes(); // Refresh the list
      },
      (error) => {
        console.error('Error adding plateforme', error);
      }
    );
  }

  
  

  modifyPlateforme(id: string, plateforme: any): void {
    this.selectedPlateforme = { ...plateforme }; // Store the plateforme to be modified in the modal
    this.isModalOpen = true; // Open the modal
  }

  updatePlateforme(): void {
    if (this.selectedPlateforme) {
      const plateformeId = this.selectedPlateforme.id.split('/').pop()?.split('.')[0];
  
      console.log('Plateforme to be updated:', this.selectedPlateforme);
      console.log('Extracted ID:', plateformeId);
  
      if (plateformeId) {
        this.plateformeService.modifyPlateforme(plateformeId, this.selectedPlateforme).subscribe(
          (response: string) => { // Now explicitly expecting a string
            console.log(response); // Success message from the server
            this.fetchPlateformes(); // Refresh the list
            this.closeModal();
          },
          (error: HttpErrorResponse) => {
            console.error('Error modifying plateforme', error);
            console.error('Error body:', error.error);
          }
        );
      } else {
        console.error('Invalid ID format');
      }
    }
  }

  deletePlateforme(id: string): void {
    const uniqueId = id.split('/').pop();
    if (uniqueId) {
      this.plateformeService.deletePlateforme(uniqueId).subscribe(
        (response: string) => { // Now explicitly expecting a string
          console.log('Plateforme deleted', response);
          this.fetchPlateformes(); // Refresh the list
        },
        (error: HttpErrorResponse) => {
          console.error('Error deleting plateforme', error);
        }
      );
    } else {
      console.error('Invalid ID format. Could not extract unique ID.');
    }
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.selectedPlateforme = null;
  }
}
