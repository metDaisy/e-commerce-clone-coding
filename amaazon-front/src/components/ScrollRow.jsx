import { useRef } from 'react';
import './ScrollRow.css';

function ScrollRow({ section }) {
  const scrollRef = useRef(null);

  const scroll = (direction) => {
    if (scrollRef.current) {
      const scrollAmount = 300;
      scrollRef.current.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth',
      });
    }
  };

  return (
    <div className="scroll-row" id={`scroll-row-${section.title.replace(/\s+/g, '-').toLowerCase()}`}>
      <h3 className="scroll-row__title">{section.title}</h3>
      <div className="scroll-row__container">
        <button
          className="scroll-row__arrow scroll-row__arrow--left"
          onClick={() => scroll('left')}
          aria-label="Scroll left"
        >
          ‹
        </button>
        <div className="scroll-row__track" ref={scrollRef}>
          {section.items.map((item, idx) => (
            <div className="scroll-row__item" key={idx}>
              <div className="scroll-row__item-emoji">{item.emoji}</div>
              <span className="scroll-row__item-name">{item.name}</span>
              {item.price && (
                <span className="scroll-row__item-price">{item.price}</span>
              )}
            </div>
          ))}
        </div>
        <button
          className="scroll-row__arrow scroll-row__arrow--right"
          onClick={() => scroll('right')}
          aria-label="Scroll right"
        >
          ›
        </button>
      </div>
    </div>
  );
}

export default ScrollRow;
