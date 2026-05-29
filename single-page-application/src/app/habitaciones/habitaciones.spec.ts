import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Habitaciones } from './habitaciones';

describe('Habitaciones', () => {
  let component: Habitaciones;
  let fixture: ComponentFixture<Habitaciones>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Habitaciones],
    }).compileComponents();

    fixture = TestBed.createComponent(Habitaciones);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
