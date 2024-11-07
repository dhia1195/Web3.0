import { TestBed } from '@angular/core/testing';

import { PlatefromeService } from '../app/services/platefrome.service';

describe('PlatefromeService', () => {
  let service: PlatefromeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PlatefromeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
