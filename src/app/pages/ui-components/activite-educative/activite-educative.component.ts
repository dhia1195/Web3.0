import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ActiviteEducativeService } from 'src/app/services/activite-educative.service';

@Component({
  selector: 'app-activite-educative',
  templateUrl: './activite-educative.component.html',
  styleUrl: './activite-educative.component.scss'
})
export class ActiviteEducativeComponent {
  activite_educative: any[] = [];
  selectedActivite: any = null;  // Used for modifying existing evaluation
 
  isModalOpen: boolean = false;

  constructor(private activitService: ActiviteEducativeService) {}

  ngOnInit(): void {
    this.fetchActivite();
  }

  fetchActivite(): void {
    this.activitService.getAllActivite().subscribe(
      (data) => {
        this.activite_educative = data;
        console.log('Parsed activiteservice data:', data);
      },
      (error) => {
        console.error('Error fetching activiteservice', error);
      }
    );
  }

  addActivite(form: NgForm) {

    const newEvaluationData = form.value;
    this.activitService.addCours(newEvaluationData).subscribe(
      (response) => {
        console.log('Evaluation added successfully', response);
        form.reset();  // Reset form after submission
        this.fetchActivite();  // Refresh the evaluations list
      },
      (error) => {
        console.error('Error adding evaluation', error);
      }
    );
  }
  modifyActivite(id: string, activite_educative: any[]): void {
    // Find the evaluation based on the clicked ID and assign it to selectedEvaluation
    this.selectedActivite = { ...activite_educative.find(activite_educative => activite_educative.id === id) };  
    this.isModalOpen = true;  // Open the modal for editing
    console.log('Selected Evaluation:', this.selectedActivite);  // Log to verify data
  }
  
  updateActivite(): void {
    if (this.selectedActivite) {
      const evaluationId = this.selectedActivite.activite_educative.split('#').pop() // Use the correct id format
      if (evaluationId) {
        this.activitService.modifyactivite_educative(evaluationId, this.selectedActivite).subscribe(
          (response: string) => {
            console.log(response);  // Success message from the server
            this.fetchActivite();  // Refresh the evaluations list
            this.closeModal();  // Close the modal
          },
          (error: HttpErrorResponse) => {

            console.error('Error modifying evaluation', error);
          }
        );
      } else {
        console.error('Invalid ID format');
      }
    }
  }
  

  deleteActivite(activite_educative: any): void {
    console.log('cours deleted', activite_educative);
    const uniqueId = activite_educative.activite_educative.split('#').pop();
   
    if (uniqueId) {
      this.activitService.deleteactivite_educative(uniqueId).subscribe(
        (response: string) => {
          console.log('Evaluation deleted', response);
          this.fetchActivite();  // Refresh the evaluations list
        },
        (error: HttpErrorResponse) => {
          console.error('Error deleting evaluation', error);
        }
      );
    } else {
      console.error('Invalid ID format. Could not extract unique ID.');
    }
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.selectedActivite = null;
  }
}