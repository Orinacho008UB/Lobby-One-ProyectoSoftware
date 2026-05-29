import { TestBed } from '@angular/core/testing';

import { SessionManager } from './session-manager';

describe('SessionManager', () => {
  let service: SessionManager;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SessionManager);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
