import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideApiConfiguration } from '../app/core/api/api-configuration';
import { provideUiTesting } from './ui-providers';

export interface PageHarness<T> {
  fixture: ComponentFixture<T>;
  backend: HttpTestingController;
  navigate: ReturnType<typeof vi.spyOn>;
  navigateTo: ReturnType<typeof vi.spyOn>;
  root: () => HTMLElement;
}

export function createPage<T>(component: Type<T>, inputs: Record<string, unknown> = {}): PageHarness<T> {
  TestBed.configureTestingModule({
    imports: [component],
    providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideApiConfiguration(''), ...provideUiTesting()],
  });
  const router = TestBed.inject(Router);
  const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  const navigateTo = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(component);
  Object.entries(inputs).forEach(([name, value]) => fixture.componentRef.setInput(name, value));
  fixture.detectChanges();
  return {
    fixture,
    backend: TestBed.inject(HttpTestingController),
    navigate,
    navigateTo,
    root: () => fixture.nativeElement as HTMLElement,
  };
}

export function setValue(root: HTMLElement, selector: string, value: string): void {
  const element = root.querySelector(selector) as HTMLInputElement | HTMLSelectElement;
  element.value = value;
  element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input'));
}
