import { useState, useEffect, useCallback } from 'react';
import heroBannerToys from '@/assets/hero_banner_toys.png';
import heroBannerSummer from '@/assets/hero_banner_summer.png';
import heroBannerElectronics from '@/assets/hero_banner_electronics.png';

export interface Banner {
  id: number;
  src: string;
  alt: string;
}

export const banners: Banner[] = [
  { id: 1, src: heroBannerToys, alt: 'Toys for little ones' },
  { id: 2, src: heroBannerSummer, alt: 'Summer Deals' },
  { id: 3, src: heroBannerElectronics, alt: 'Top Electronics' },
];

export const useHeroBanner = () => {
  const [current, setCurrent] = useState(0);

  const nextSlide = useCallback(() => {
    setCurrent((prev) => (prev + 1) % banners.length);
  }, []);

  const prevSlide = useCallback(() => {
    setCurrent((prev) => (prev - 1 + banners.length) % banners.length);
  }, []);

  // Auto-slide every 5 seconds
  useEffect(() => {
    const timer = setInterval(nextSlide, 5000);
    return () => clearInterval(timer);
  }, [nextSlide]);

  return {
    current,
    setCurrent,
    nextSlide,
    prevSlide,
    banners,
  };
};
