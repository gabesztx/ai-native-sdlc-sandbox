import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FieldTree, FormField, FormRoot, form, required } from '@angular/forms/signals';
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
  imports: [FormField, FormRoot, Button, Card, InputText, Message, Select, ThemeToggleComponent],
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

  private adminModel = signal({ email: '', password: '' });
  public adminForm = form(this.adminModel, (path) => {
    required(path.email);
    required(path.password);
  });

  private memberModel = signal<{ memberId: number | null }>({ memberId: null });
  public memberForm = form(this.memberModel, (path) => {
    required(path.memberId);
  });

  public isInvalid<T>(field: FieldTree<T>): boolean {
    return field().invalid() && field().touched();
  }

  public onAdminLogin(): void {
    if (this.adminLoading()) {
      return;
    }
    if (this.adminForm().invalid()) {
      this.adminForm().markAsTouched();
      return;
    }
    const { email, password } = this.adminModel();
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
    const memberId = this.memberModel().memberId;
    if (memberId === null) {
      this.memberForm().markAsTouched();
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
