import { Component, OnInit } from '@angular/core';
import { TechnologieEducativeService } from 'src/app/services/technologie-educative.service';

@Component({
  selector: 'app-technologie-educative',
  templateUrl: './technologie-educative.component.html',
  styleUrls: ['./technologie-educative.component.scss']
})
export class TechnologieEducativeComponent implements OnInit {
  technologies: any[] = []; // To hold fetched technologies
  newTechnology: any = { nom: '', impactEnvironnemental: '' }; // Model for new technology

  constructor(private technologieService: TechnologieEducativeService) {}

  ngOnInit(): void {
    this.fetchTechnologies();
  }

  // Fetch all technologies from the service
  fetchTechnologies() {
    this.technologieService.getAllTechnologies().subscribe(
      (data: any) => {
        this.technologies = data; // Update the technologies list
      },
      (error) => {
        console.error('Error fetching technologies:', error);
      }
    );
  }

  // Add a new technology using the service
  addTechnologie() {
    if (this.newTechnology.nom && this.newTechnology.impactEnvironnemental) {
      this.technologieService.addTechnologieEduc(this.newTechnology).subscribe(
        (response: string) => {
          console.log('Technology added successfully:', response);
          this.fetchTechnologies(); // Refresh the technology list after adding
          this.newTechnology = { nom: '', impactEnvironnemental: '' }; // Reset form
        },
        (error) => {
          console.error('Error adding technology:', error);
        }
      );
    } else {
      console.error('Please provide valid technology details');
    }
  }
}
