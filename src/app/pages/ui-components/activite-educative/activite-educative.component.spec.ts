import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActiviteEducativeComponent } from './activite-educative.component';

describe('ActiviteEducativeComponent', () => {
  let component: ActiviteEducativeComponent;
  let fixture: ComponentFixture<ActiviteEducativeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActiviteEducativeComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ActiviteEducativeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
