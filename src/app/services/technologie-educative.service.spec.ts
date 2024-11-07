import { TestBed } from '@angular/core/testing';

import { TechnologieEducativeService } from './technologie-educative.service';

describe('TechnologieEducativeService', () => {
  let service: TechnologieEducativeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TechnologieEducativeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
