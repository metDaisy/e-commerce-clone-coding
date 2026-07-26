import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const signInSchema = z.object({
  email: z.string().min(1, 'Enter your email or phone number'),
  password: z.string().min(1, 'Enter your password'),
});

export type SignInFormData = z.infer<typeof signInSchema>;

export const useSignInForm = () => {
  const form = useForm<SignInFormData>({
    resolver: zodResolver(signInSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = (data: SignInFormData) => {
    console.log('Login submitted:', data);
    // TODO: API 호출 로직 추가
  };

  return {
    form,
    onSubmit: form.handleSubmit(onSubmit),
  };
};