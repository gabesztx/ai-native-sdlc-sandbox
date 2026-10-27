import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, catchError, finalize } from 'rxjs';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { AuthTokens } from '../../../../core/api/models';
import { AuthStoreService } from '../../../../core/auth/auth-store.service';
import { LoginService } from '../../services/login.service';
import { ThemeToggleComponent } from '../../../../shared/components/theme-toggle/theme-toggle.component';

@Component({
  selector: 'login-page',
  imports: [ReactiveFormsModule, Button, Card, InputText, Message, Select, ThemeToggleComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
})
export class LoginPageComponent {
  private loginService = inject(LoginService);
  private store = inject(AuthStoreService);
  private router = inject(Router);

  public members = toSignal(this.loginService.teamMembers().pipe(catchError(() => EMPTY)), {
    initialValue: [],
  });
  public adminError = signal('');
  public memberError = signal('');
  public adminLoading = signal(false);
  public memberLoading = signal(false);

  public adminForm = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  public memberForm = new FormGroup({
    memberId: new FormControl<number | null>(null, Validators.required),
  });

  public onAdminLogin(): void {
    if (this.adminLoading()) {
      return;
    }
    if (this.adminForm.invalid) {
      this.adminForm.markAllAsTouched();
      return;
    }
    const { email, password } = this.adminForm.getRawValue();
    this.adminError.set('');
    this.adminLoading.set(true);
    this.loginService
      .loginAdmin(email, password)
      .pipe(
        catchError(() => {
          this.adminError.set('Hibás e-mail cím vagy jelszó');
          return EMPTY;
        }),
        finalize(() => this.adminLoading.set(false)),
      )
      .subscribe((tokens) => this.enter(tokens));
  }

  public onMemberLogin(): void {
    if (this.memberLoading()) {
      return;
    }
    const memberId = this.memberForm.controls.memberId.value;
    if (memberId === null) {
      this.memberForm.markAllAsTouched();
      return;
    }
    this.memberError.set('');
    this.memberLoading.set(true);
    this.loginService
      .loginMember(memberId)
      .pipe(
        catchError(() => {
          this.memberError.set('A belépés nem sikerült');
          return EMPTY;
        }),
        finalize(() => this.memberLoading.set(false)),
      )
      .subscribe((tokens) => this.enter(tokens));
  }

  private enter(tokens: AuthTokens): void {
    this.store.save(tokens);
    void this.router.navigateByUrl(this.store.homePath());
  }
}
