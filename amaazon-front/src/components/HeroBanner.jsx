import { useState, useEffect, useCallback } from 'react';
import './HeroBanner.css';

import heroBannerToys from '../assets/hero_banner_toys.png';
import heroBannerSummer from '../assets/hero_banner_summer.png';
import heroBannerElectronics from '../assets/hero_banner_electronics.png';

const banners = [
  { id: 1, src: heroBannerToys, alt: 'Toys for little ones' },
  { id: 2, src: heroBannerSummer, alt: 'Summer Deals' },
  { id: 3, src: heroBannerElectronics, alt: 'Top Electronics' },
];

function HeroBanner() {
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

  return (
    <div className="hero" id="hero-banner">
      <div className="hero__slider">
        {banners.map((banner, index) => (
          <div
            key={banner.id}
            className={`hero__slide ${index === current ? 'hero__slide--active' : ''}`}
          >
            <img src={banner.src} alt={banner.alt} className="hero__image" />
          </div>
        ))}

        {/* Navigation Arrows */}
        <button
          className="hero__arrow hero__arrow--left"
          onClick={prevSlide}
          id="hero-prev"
          aria-label="Previous slide"
        >
          ‹
        </button>
        <button
          className="hero__arrow hero__arrow--right"
          onClick={nextSlide}
          id="hero-next"
          aria-label="Next slide"
        >
          ›
        </button>
      </div>

      {/* Bottom gradient fade (Amazon-style) */}
      <div className="hero__fade" />

      {/* Dots indicator */}
      <div className="hero__dots">
        {banners.map((_, index) => (
          <button
            key={index}
            className={`hero__dot ${index === current ? 'hero__dot--active' : ''}`}
            onClick={() => setCurrent(index)}
            aria-label={`Go to slide ${index + 1}`}
          />
        ))}
      </div>
    </div>
  );
}

export default HeroBanner;
