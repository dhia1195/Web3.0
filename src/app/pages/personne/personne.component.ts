import { Component, ViewEncapsulation, ViewChild } from '@angular/core';
import {
  ApexChart,
  ChartComponent,
  ApexDataLabels,
  ApexLegend,
  ApexStroke,
  ApexTooltip,
  ApexAxisChartSeries,
  ApexXAxis,
  ApexYAxis,
  ApexGrid,
  ApexPlotOptions,
  ApexFill,
  ApexMarkers,
  ApexResponsive,
} from 'ng-apexcharts';
import { PersonneService } from '../../services/personne.service';
interface month {
  value: string;
  viewValue: string;
}
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

export interface salesOverviewChart {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  dataLabels: ApexDataLabels;
  plotOptions: ApexPlotOptions;
  yaxis: ApexYAxis;
  xaxis: ApexXAxis;
  fill: ApexFill;
  tooltip: ApexTooltip;
  stroke: ApexStroke;
  legend: ApexLegend;
  grid: ApexGrid;
  marker: ApexMarkers;
}

export interface yearlyChart {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  dataLabels: ApexDataLabels;
  plotOptions: ApexPlotOptions;
  tooltip: ApexTooltip;
  stroke: ApexStroke;
  legend: ApexLegend;
  responsive: ApexResponsive;
}

export interface monthlyChart {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  dataLabels: ApexDataLabels;
  plotOptions: ApexPlotOptions;
  tooltip: ApexTooltip;
  stroke: ApexStroke;
  legend: ApexLegend;
  responsive: ApexResponsive;
}

interface stats {
  id: number;
  time: string;
  color: string;
  title?: string;
  subtext?: string;
  link?: string;
}

export interface productsData {
  id: number;
  imagePath: string;
  uname: string;
  position: string;
  productName: string;
  budget: number;
  priority: string;
}

// ecommerce card
interface productcards {
  id: number;
  imgSrc: string;
  title: string;
  price: string;
  rprice: string;
}


const ELEMENT_DATA: any = [
  {
    id: 1,
    imagePath: 'assets/images/profile/user-1.jpg',
    nom: 'aziz',
    age : 25
   
    
    
  },
 
 

];
@Component({
  selector: 'app-personne',
  templateUrl: './personne.component.html',
  styleUrl: './personne.component.scss',
  encapsulation: ViewEncapsulation.None,
  
})
export class PersonneComponent {
  personneForm: FormGroup;
  isUpdating = false; // Flag pour indiquer si on est en mode mise à jour
  currentPersonneId: string | null = null;
  dataSource = ELEMENT_DATA;
  roles: string[] = ['administrateur', 'etudiant', 'enseignant', 'parent'];
  displayedColumns: string[] = ['nom', 'age', 'dateNaissance','niveau','role','est_parent_de','estSupervisePar','update', 'delete'];
  constructor(private personneService: PersonneService,private fb: FormBuilder) {
    this.personneForm = this.fb.group({
      nom: ['', Validators.required],
      age: ['', Validators.required],
      dateNaissance: ['', Validators.required],
      niveau: ['', Validators.required],
      role: ['', Validators.required],
      // Add other form controls as needed
    });
  }
  ngOnInit(): void {
    this.personneService.getPersonnes().subscribe(
      data => {
        this.dataSource = data.map((item: any) => {
          
          const parentItem = data.find((e: any) =>{
            console.log(e.personne)
            console.log(e.personne === item.est_parent_de)
            return e.personne === item.est_parent_de
          })
          console.log(parentItem)
          const suppItem = data.find((e: any) =>{
            console.log(e.personne)
            console.log(e.personne === item.estSuperviséPar)
            return e.personne === item.estSuperviséPar
          })
          return {
            ...item,
            estSupervisePar: suppItem ? suppItem.nom : null , // Retain original field if it's already available
            nom_fils: parentItem ? parentItem.nom : null // Handle cases where no match is found
          };
        });
      }
      ,
      error => console.error('Erreur lors de la récupération des personnes :', error)
    );
  }
  addPersonne() {
    if (this.personneForm.valid) {
      const newPerson = {
  // Generate a unique ID (you can use UUID in real apps)
        ...this.personneForm.value
      };
      this.personneService.addPersonne(newPerson).subscribe(

      )
       // Add the new person to the data source
       this.ngOnInit()
      // this.personneForm.reset();  // Reset the form after adding the person
      console.log('Nouvelle personne ajoutée :', newPerson);
    } else {
      console.log('Le formulaire n\'est pas valide');
    }
    console.log('Ajout d\'une nouvelle personne');
  }
  
  editPersonne(id: string) {
    // Logique pour mettre à jour une personne existante
    const identifier = id.split('#').pop() || id; // This ensures it only takes the part after '#'
  console.log('Suppression de la personne avec ID :', identifier);
    this.isUpdating=true
    this.currentPersonneId=identifier
    const editE=this.dataSource.find((element:any)=>element.personne === id)
    console.log(editE)
    this.personneForm.setValue({
      nom: editE.nom,
      age: editE.age,
      dateNaissance: editE.dateNaissance || '', // Add default if property is missing
      niveau: editE.niveau || '', // Add default if property is missing
      role: editE.role.toLowerCase() || '' // Add default if property is missing
    });
  }
  
  deletePersonne(id: string) {
    // Logique pour supprimer une personne par ID
    const identifier = id.split('#').pop() || id; // This ensures it only takes the part after '#'
  console.log('Suppression de la personne avec ID :', identifier);
     this.personneService.deletePersonne(identifier).subscribe();
     this.ngOnInit()
  }
  onSubmit(): void {
    if (this.isUpdating) {
      this.personneForm.patchValue({
        role: this.personneForm.get('role')?.value.charAt(0).toUpperCase() + this.personneForm.get('role')?.value.slice(1).toLowerCase()
      });
      
      console.log(this.personneForm.value)
      console.log(this.currentPersonneId)
     
      this.personneService.updatePersonne(this.currentPersonneId,this.personneForm.value).subscribe()
      this.ngOnInit()
      // this.editPersonne()
    } else {
      this.addPersonne();
    }
  }
}
