import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TechnologieEducativeComponent } from './technologie-educative.component';

describe('TechnologieEducativeComponent', () => {
  let component: TechnologieEducativeComponent;
  let fixture: ComponentFixture<TechnologieEducativeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TechnologieEducativeComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TechnologieEducativeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
