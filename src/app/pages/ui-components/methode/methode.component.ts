import { Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MethodeService } from 'src/app/services/methode.service';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-methode',
  templateUrl: './methode.component.html',
  styleUrls: ['./methode.component.scss']
})
export class MethodeComponent {
  
  methodes: any[] = [];
  selectedMethode: any = null; 
  isModalOpen: boolean = false; 

  constructor(private methodeService: MethodeService) {}

  ngOnInit(): void {
    this.fetchMethodes();
  }

  fetchMethodes(): void {
    this.methodeService.getAllMethodes().subscribe(
      (data) => {
        this.methodes = data.results; // Refreshing the list of plateformes
      },
      (error) => {
        console.error('Error fetching methodes', error);
      }
    );
  }
  addMethode(form: NgForm) {
    const newMethode = form.value;
    this.methodeService.addMethode(newMethode).subscribe(
      (response) => {
        console.log('Methode added successfully', response);
        form.reset(); // Reset the form after submission
        this.fetchMethodes(); // Refresh the list
      },
      (error) => {
        console.error('Error adding plateforme', error);
      }
    );
  }

  
  

  modifyMethode(id: string, methode: any): void {
    this.selectedMethode = { ...methode }; // Store the plateforme to be modified in the modal
    this.isModalOpen = true; // Open the modal
  }

  updateMethode(): void {
    if (this.selectedMethode) {
      const methodeId = this.selectedMethode.id.split('/').pop()?.split('.')[0];
  
      console.log('Methode to be updated:', this.selectedMethode);
      console.log('Extracted ID:', methodeId);
  
      if (methodeId) {
        this.methodeService.modifyMethode(methodeId, this.selectedMethode).subscribe(
          (response: string) => { // Now explicitly expecting a string
            console.log(response); // Success message from the server
            this.fetchMethodes(); // Refresh the list
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

  deleteMethode(id: string): void {
    const uniqueId = id.split('/').pop();
    if (uniqueId) {
      this.methodeService.deleteMethode(uniqueId).subscribe(
        (response: string) => { // Now explicitly expecting a string
          console.log('Methode deleted', response);
          this.fetchMethodes(); // Refresh the list
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
    this.selectedMethode = null;
  }
}
