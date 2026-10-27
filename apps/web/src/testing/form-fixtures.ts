import { FormDetail, FormResponse } from '../app/api/models';

export const TEAM = [
  { id: 3, name: 'Teszt Elek' },
  { id: 4, name: 'Minta Anna' },
  { id: 5, name: 'Próba Péter' },
];

export function draftForm(): FormDetail {
  return {
    id: 7,
    topic: 'Csapatmunka Q4',
    evaluatedMember: TEAM[0],
    status: 'DRAFT',
    createdAt: '2026-10-27T10:00:00Z',
    recipientCount: 0,
    submittedCount: 0,
    questions: [
      { id: 11, position: 1, text: 'Mennyire segítőkész?', type: 'SCALE', required: true, scaleMin: 1, scaleMax: 5 },
      { id: 12, position: 2, text: 'Miben fejlődhetne?', type: 'TEXT', required: false },
    ],
    recipients: [],
  };
}

export function sentForm(): FormDetail {
  return {
    ...draftForm(),
    status: 'SENT',
    sentAt: '2026-10-27T11:00:00Z',
    recipientCount: 2,
    submittedCount: 1,
    recipients: [
      { member: TEAM[1], status: 'SUBMITTED', submittedAt: '2026-10-27T12:00:00Z' },
      { member: TEAM[2], status: 'PENDING' },
    ],
  };
}

export function annasResponse(): FormResponse {
  return {
    member: TEAM[1],
    submittedAt: '2026-10-27T12:00:00Z',
    answers: [
      { questionId: 11, scaleValue: 4 },
      { questionId: 12, textValue: 'Több dokumentáció' },
    ],
  };
}
