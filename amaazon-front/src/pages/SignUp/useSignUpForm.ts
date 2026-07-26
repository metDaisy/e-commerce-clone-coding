import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const signUpSchema = z.object({
  name: z.string().min(1, 'Enter your name'),
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(6, 'Passwords must be at least 6 characters.'),
  confirmPassword: z.string().min(1, 'Type your password again'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords must match',
  path: ['confirmPassword'],
});

export type SignUpFormData = z.infer<typeof signUpSchema>;

export const useSignUpForm = () => {
  const form = useForm<SignUpFormData>({
    resolver: zodResolver(signUpSchema),
    defaultValues: {
      name: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  });

  const onSubmit = (data: SignUpFormData) => {
    console.log('Sign up submitted:', data);
    // TODO: API 호출 로직 추가
  };

  return {
    form,
    onSubmit: form.handleSubmit(onSubmit),
  };
};