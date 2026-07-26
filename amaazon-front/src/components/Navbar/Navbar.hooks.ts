import { useState } from 'react';

export const useNavbar = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');

  const categories = [
    'All', 'Arts & Crafts', 'Automotive', 'Baby', 'Beauty & Personal Care',
    'Books', 'Clothing', 'Computers', 'Electronics', 'Garden',
    'Health', 'Home', 'Kitchen', 'Movies & TV', 'Music',
    'Pet Supplies', 'Sports', 'Tools', 'Toys & Games', 'Video Games',
  ];

  return {
    searchQuery,
    setSearchQuery,
    selectedCategory,
    setSelectedCategory,
    categories,
  };
};
