import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';

import { NgForm } from '@angular/forms';
import { EvaluationService } from 'src/app/services/evaluation.service';

@Component({
  selector: 'app-evaluation',
  templateUrl: './evaluation.component.html',
  styleUrl: './evaluation.component.scss'
})
export class EvaluationComponent {
  evaluations: any[] = [];
  selectedEvaluation: any = null;  // Used for modifying existing evaluation
 
  isModalOpen: boolean = false;

  constructor(private evaluationService: EvaluationService) {}

  ngOnInit(): void {
    this.fetchEvaluation();
  }

  fetchEvaluation(): void {
    this.evaluationService.getAllEvaluation().subscribe(
      (data) => {
        this.evaluations = data;
        console.log('Parsed evaluations data:', data);
      },
      (error) => {
        console.error('Error fetching evaluations', error);
      }
    );
  }

  addEvaluation(form: NgForm) {
    const newEvaluationData = form.value;
    this.evaluationService.addEvaluation(newEvaluationData).subscribe(
      (response) => {
        console.log('Evaluation added successfully', response);
        form.reset();  // Reset form after submission
        this.fetchEvaluation();  // Refresh the evaluations list
      },
      (error) => {
        console.error('Error adding evaluation', error);
      }
    );
  }
  modifyEvaluation(id: string, evaluations: any[]): void {
    // Find the evaluation based on the clicked ID and assign it to selectedEvaluation
    this.selectedEvaluation = { ...evaluations.find(evaluation => evaluation.id === id) };  
    this.isModalOpen = true;  // Open the modal for editing
    console.log('Selected Evaluation:', this.selectedEvaluation);  // Log to verify data
  }
  
  updateEvaluation(): void {
    if (this.selectedEvaluation) {
      const evaluationId = this.selectedEvaluation.Evaluation.split('#').pop() // Use the correct id format
      if (evaluationId) {
        this.evaluationService.modifyEvaluation(evaluationId, this.selectedEvaluation).subscribe(
          (response: string) => {
            console.log(response);  // Success message from the server
            this.fetchEvaluation();  // Refresh the evaluations list
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
  

  deleteEvaluation(evaluation: any): void {
    console.log('cours deleted', evaluation);
    const uniqueId = evaluation.Evaluation.split('#').pop();
   
    if (uniqueId) {
      this.evaluationService.deleteEvaluation(uniqueId).subscribe(
        (response: string) => {
          console.log('Evaluation deleted', response);
          this.fetchEvaluation();  // Refresh the evaluations list
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
    this.selectedEvaluation = null;
  }
}
