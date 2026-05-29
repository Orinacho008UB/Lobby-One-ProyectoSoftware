import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Perfiles } from './perfiles';

describe('Perfiles', () => {
  let component: Perfiles;
  let fixture: ComponentFixture<Perfiles>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Perfiles],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfiles);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
