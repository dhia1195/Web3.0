import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { NgForm } from '@angular/forms';
import { CoursService } from 'src/app/services/cours.service';

@Component({
  selector: 'app-cours',
  templateUrl: './cours.component.html',
  styleUrl: './cours.component.scss'
})
export class CoursComponent {
  cours: any[] = [];
  selectedCours: any = null; 
  isModalOpen: boolean = false; 

  constructor(private coursservice: CoursService) {}

  ngOnInit(): void {
    this.fetchCours();
  }

  fetchCours(): void {
    this.coursservice.getAllCours().subscribe(
      (data) => {
        this.cours = data; 
        console.log('Parsed courses data:', data);// Refreshing the list of plateformes
      },
      (error) => {
        console.error('Error fetching cours', error);
        
      }
    );
  }
  addCours(form: NgForm) {
    const newCoursforme = form.value;
    this.coursservice.addCours(newCoursforme).subscribe(
      (response) => {
        console.log('Plateforme added successfully', response);
        form.reset(); // Reset the form after submission
        this.fetchCours(); // Refresh the list
      },
      (error) => {
        console.error('Error adding plateforme', error);
      }
    );
  }

  
  

  modifyCours(id: string, cours: any): void {
    this.selectedCours = { ...cours }; // Store the plateforme to be modified in the modal
    this.isModalOpen = true; // Open the modal
  }

  updateCours(): void {
    if (this.selectedCours) {
      const coursId = this.selectedCours.Cours.split('#').pop();
      console.log('uniqueId', coursId);
  
      console.log('cours to be updated:', this.selectedCours);
      console.log('Extracted ID:', coursId);
  
      if (coursId) {
        this.coursservice.modifyCours(coursId, this.selectedCours).subscribe(
          (response: string) => { // Now explicitly expecting a string
            console.log(response); // Success message from the server
            this.fetchCours(); // Refresh the list
            this.closeModal();
          },
          (error: HttpErrorResponse) => {
            console.error('Error modifying cours', error);
            console.error('Error body:', error.error);
          }
        );
      } else {
        console.error('Invalid ID format');
      }
    }
  }

  deleteCours(cours: any): void {
    console.log('cours deleted', cours);

    const uniqueId = cours.Cours.split('#').pop();
    if (uniqueId) {
      console.log('uniqueId', uniqueId);

      this.coursservice.deleteCours(uniqueId).subscribe(
        (response: string) => { // Now explicitly expecting a string
          console.log('cours deleted', response);
          this.fetchCours(); // Refresh the list
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
      this.selectedCours = null;
    }

  }


