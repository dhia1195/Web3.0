import { TestBed } from '@angular/core/testing';

import { ActiviteEducativeService } from './activite-educative.service';

describe('ActiviteEducativeService', () => {
  let service: ActiviteEducativeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ActiviteEducativeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
